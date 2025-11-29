import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'

// Firebase configuration is provided via Vite env variables.
// Define the following in your frontend environment (e.g. .env.local):
// VITE_FIREBASE_API_KEY, VITE_FIREBASE_AUTH_DOMAIN, VITE_FIREBASE_PROJECT_ID,
// VITE_FIREBASE_APP_ID, VITE_FIREBASE_MESSAGING_SENDER_ID (optional), VITE_FIREBASE_MEASUREMENT_ID (optional)

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY as string,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN as string,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID as string,
  appId: import.meta.env.VITE_FIREBASE_APP_ID as string,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID as string | undefined,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID as string | undefined,
}

// Validate required Firebase configuration
const requiredEnvVars = [
  'VITE_FIREBASE_API_KEY',
  'VITE_FIREBASE_AUTH_DOMAIN',
  'VITE_FIREBASE_PROJECT_ID',
  'VITE_FIREBASE_APP_ID',
]

const missingVars = requiredEnvVars.filter(
  (varName) => !import.meta.env[varName] || import.meta.env[varName] === ''
)

if (missingVars.length > 0) {
  const errorMessage = `Missing required Firebase environment variables: ${missingVars.join(', ')}\n` +
    'Please set these variables in your .env file (e.g., .env.development or .env.local)\n' +
    'After setting the variables, restart the Vite development server.'
  console.error(errorMessage)
  
  // Show user-friendly error in browser
  if (typeof window !== 'undefined') {
    console.error('Current environment mode:', import.meta.env.MODE)
    console.error('Available env vars:', Object.keys(import.meta.env).filter(k => k.startsWith('VITE_')))
  }
  
  // Throw error to prevent Firebase initialization with invalid config
  throw new Error(errorMessage)
}

// Validate that values are not placeholder values
const placeholderValues = ['your-firebase-api-key-here', 'your-project-id.firebaseapp.com', 'your-project-id', 'your-app-id-here']
const hasPlaceholder = Object.values(firebaseConfig).some(
  (value) => typeof value === 'string' && placeholderValues.some(placeholder => value.includes(placeholder))
)

if (hasPlaceholder) {
  const errorMessage = 'Firebase configuration contains placeholder values. Please replace them with actual Firebase credentials from Firebase Console.'
  console.error(errorMessage)
  throw new Error(errorMessage)
}

let app
try {
  app = initializeApp(firebaseConfig)
  console.log('Firebase initialized successfully')
} catch (error) {
  console.error('Failed to initialize Firebase:', error)
  throw error
}

export const auth = getAuth(app)


