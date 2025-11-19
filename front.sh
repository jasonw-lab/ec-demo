#!/bin/bash

# Exit on error
set -e

#source ~/.bashrc
export VOLTA_HOME="$HOME/.volta"
export PATH="$VOLTA_HOME/bin:$PATH"
echo "node version: $(node -v)"
echo "npm version: $(npm -v)"

echo "Starting build process for frontend..."



# Navigate to the frontend directory
git pull
cd front

# Install dependencies
echo "Installing dependencies..."
pnpm install

# Build the project
echo "Building the project..."
pnpm build

# Create target directory if it doesn't exist
echo "Creating target directory if it doesn't exist..."
rm -rf /mydata/nginx/html/ec-demo
mkdir -p /mydata/nginx/html/ec-demo

# Copy the built assets to the target directory
echo "Copying built assets to /mydata/nginx/html/ec-demo..."
cp -r dist/* /mydata/nginx/html/ec-demo/

echo "Build and deployment completed successfully!"