# NOTE: This Dockerfile is no longer used. The frontend is now built directly on the VPS
# and served by the main nginx container. This file is kept for reference only.

# Build stage
FROM node:18-alpine as build-stage

WORKDIR /app

# Copy package.json and pnpm-lock.yaml
COPY package.json pnpm-lock.yaml ./

# Install pnpm
RUN npm install -g pnpm

# Install dependencies
RUN pnpm install

# Copy the rest of the application
COPY . .

# Build the application
RUN pnpm build

# Production stage
FROM nginx:stable-alpine as production-stage

# Copy built files from build stage
COPY --from=build-stage /app/dist /usr/share/nginx/html

# Use default nginx configuration
# We'll configure nginx in docker-compose with volumes

# Expose port 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
