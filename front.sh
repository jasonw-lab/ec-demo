#!/bin/bash

set -euo pipefail

source "$HOME/.bashrc"
export BASEPATH="${BASEPATH:-/mydata2}"
FRONTEND_DIR="${BASEPATH}/nginx/html/ec-demo"

export VOLTA_HOME="$HOME/.volta"
export PATH="$VOLTA_HOME/bin:$PATH"
echo "node version: $(node -v)"
echo "npm version: $(npm -v)"
echo "BASEPATH: $BASEPATH"

echo "Starting build process for frontend..."
# Navigate to the frontend directory
# git pull
cd apps/web

# Install dependencies
echo "Installing dependencies..."
pnpm install

# Build the project
echo "Building the project..."
pnpm run build -- --mode production
# npm run build 

# Create target directory if it doesn't exist
echo "Creating target directory if it doesn't exist..."
rm -rf "$FRONTEND_DIR"
mkdir -p "$FRONTEND_DIR"

# Copy the built assets to the target directory
echo "Copying built assets to $FRONTEND_DIR..."
cp -r dist/* "$FRONTEND_DIR/"

echo "Build and deployment completed successfully!"
