# PFIP Microservices - Frontend Service

This is the Angular frontend for the PFIP Microservices project. It is built using Angular 17, Tailwind CSS, Angular Material, and Three.js for 3D visualization.

## 🚀 Technologies Used
- **Angular 17** - Core Framework
- **TypeScript** - Programming Language
- **Angular Material** - UI Components
- **Tailwind CSS** - Utility-first styling & responsiveness
- **RxJS** - Reactive programming for API calls
- **Three.js** - 3D Dashboard Visualization

## 📁 Features Implemented
- **Authentication**: Login and Registration forms with JWT token management via `AuthGuard` and `AuthInterceptor`.
- **Dashboard**: Modern overview with 3D pillars using Three.js to represent deposits, withdrawals, and transfers.
- **Transactions**: Paged data table for history with sorting and a detailed view page.
- **User Profile**: Display authenticated user details.
- **Modern UI**: Immersive gradients, animations, glassmorphism UI elements, and fully responsive layout.

## ⚙️ Configuration
The frontend communicates directly with the **API Gateway** running on port `8080`.
Ensure that the API gateway, `user-service`, `auth-service`, and `transaction-service` are all running before starting the frontend.

You can configure the backend URL in:
- `src/environments/environment.ts` (Development)
- `src/environments/environment.prod.ts` (Production)

## 🛠️ How to Run

1. **Install Dependencies**
   Navigate to the `frontend-service` folder and install dependencies using legacy-peer-deps to avoid any Angular/CDK version conflicts:
   ```bash
   cd frontend-service
   npm install --legacy-peer-deps
   ```

2. **Start the Development Server**
   ```bash
   npx ng serve
   ```
   Or, if you have Angular CLI installed globally:
   ```bash
   ng serve
   ```

3. **Open the Application**
   Navigate to `http://localhost:4200/` in your browser.

## 🧪 Development Notes
- **Tailwind CSS**: Pre-configured in `tailwind.config.js`. Uses custom brand colors (green, blue, red) and modern animations (`animate-blob`).
- **Three.js**: Integrated directly inside the `DashboardComponent` to render the transaction volume pillars.

## 🛑 Important Rules
- Do NOT touch existing backend microservices.
- Ensure CORS is configured properly on the API Gateway to allow requests from `http://localhost:4200`.
