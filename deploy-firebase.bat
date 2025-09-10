@echo off
echo 🔥 Deploying Firebase Configuration for Plateful Reviews System
echo.

echo ⏳ Checking Firebase CLI installation...
firebase --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Firebase CLI not found. Please install it first:
    echo    npm install -g firebase-tools
    echo.
    pause
    exit /b 1
)

echo ✅ Firebase CLI found
echo.

echo 🔐 Logging into Firebase...
firebase login

echo.
echo 📊 Deploying Firestore rules and indexes...
firebase deploy --only firestore

if %errorlevel% equ 0 (
    echo.
    echo 🎉 SUCCESS! Firebase configuration deployed successfully!
    echo.
    echo ✅ Firestore security rules updated
    echo ✅ Database indexes created
    echo.
    echo 📱 Your Reviews & Rating system is now ready to use!
    echo.
    echo Next steps:
    echo 1. Run your Android app
    echo 2. Navigate to any restaurant profile
    echo 3. Check the Reviews tab - you should see sample reviews
    echo 4. Try adding your own review
    echo.
) else (
    echo.
    echo ❌ Deployment failed. Please check the error messages above.
    echo.
    echo Common issues:
    echo - Make sure you're authenticated: firebase login
    echo - Verify project selection: firebase use plateful-abin
    echo - Check internet connection
    echo.
)

echo Press any key to close...
pause >nul
