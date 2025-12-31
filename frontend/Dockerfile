# Stage 1: Build the Angular app
FROM node:24 AS build
WORKDIR /usr/src/app

# Install dependencies
COPY package*.json ./
RUN npm ci

# Copy the rest of the frontend source
COPY . .

# Build Angular for production
# With your angular.json, this will output to: dist/frontend/browser/
RUN npm run build -- --configuration production

# Stage 2: Serve the built app with Nginx
FROM nginx:stable-alpine

# Remove default nginx site config
RUN rm /etc/nginx/conf.d/default.conf

# Copy our custom nginx config from the project
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy the built Angular files from the build stage
# NOTE: angular.json + @angular/build:application → dist/frontend/browser by default
COPY --from=build /usr/src/app/dist/frontend/browser/ /usr/share/nginx/html

# Nginx listens on port 80
EXPOSE 80
