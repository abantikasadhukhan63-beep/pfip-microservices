import { Component, ElementRef, OnInit, ViewChild, inject, OnDestroy, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionSummary } from '../../core/models/transaction.model';
import * as THREE from 'three';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('rendererContainer', { static: false }) rendererContainer!: ElementRef;

  private transactionService = inject(TransactionService);
  
  summary: TransactionSummary | null = null;
  isLoading = true;

  // Three.js properties
  private renderer!: THREE.WebGLRenderer;
  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private animationFrameId: number | null = null;
  private cubes: THREE.Mesh[] = [];

  ngOnInit() {
    this.transactionService.getTransactionSummary().subscribe({
      next: (res) => {
        if (res.success) {
          this.summary = res.data;
          this.update3DScene();
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  ngAfterViewInit() {
    this.initThreeJs();
  }

  ngOnDestroy() {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
    }
    if (this.renderer) {
      this.renderer.dispose();
    }
  }

  private initThreeJs() {
    const container = this.rendererContainer.nativeElement;

    this.scene = new THREE.Scene();
    this.camera = new THREE.PerspectiveCamera(75, container.clientWidth / container.clientHeight, 0.1, 1000);
    
    this.renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
    this.renderer.setSize(container.clientWidth, container.clientHeight);
    container.appendChild(this.renderer.domElement);

    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    this.scene.add(ambientLight);

    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
    directionalLight.position.set(10, 20, 10);
    this.scene.add(directionalLight);

    this.camera.position.z = 15;
    this.camera.position.y = 5;
    this.camera.lookAt(0, 0, 0);

    this.animate();

    window.addEventListener('resize', this.onWindowResize.bind(this));
  }

  private update3DScene() {
    if (!this.summary || !this.scene) return;

    // Clear existing cubes
    this.cubes.forEach(cube => this.scene.remove(cube));
    this.cubes = [];

    const maxVal = Math.max(this.summary.totalDeposits, this.summary.totalWithdrawals, this.summary.totalTransfers, 1);
    
    const createPillar = (value: number, color: number, xPos: number) => {
      const height = (value / maxVal) * 10 + 0.5; // normalize height between 0.5 and 10.5
      const geometry = new THREE.BoxGeometry(2, height, 2);
      const material = new THREE.MeshPhongMaterial({ 
        color, 
        transparent: true, 
        opacity: 0.8,
        shininess: 100
      });
      const cube = new THREE.Mesh(geometry, material);
      cube.position.x = xPos;
      cube.position.y = height / 2 - 5; // Base at bottom
      this.scene.add(cube);
      this.cubes.push(cube);
    };

    createPillar(this.summary.totalDeposits, 0x22c55e, -5); // Green for deposits
    createPillar(this.summary.totalWithdrawals, 0xef4444, 0); // Red for withdrawals
    createPillar(this.summary.totalTransfers, 0x3b82f6, 5); // Blue for transfers
  }

  private animate() {
    this.animationFrameId = requestAnimationFrame(this.animate.bind(this));

    // Slowly rotate the scene
    if (this.scene) {
      this.scene.rotation.y += 0.005;
    }

    if (this.renderer && this.scene && this.camera) {
      this.renderer.render(this.scene, this.camera);
    }
  }

  private onWindowResize() {
    if (this.rendererContainer && this.camera && this.renderer) {
      const container = this.rendererContainer.nativeElement;
      this.camera.aspect = container.clientWidth / container.clientHeight;
      this.camera.updateProjectionMatrix();
      this.renderer.setSize(container.clientWidth, container.clientHeight);
    }
  }
}
