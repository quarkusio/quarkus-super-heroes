# First stage builds the application
FROM registry.access.redhat.com/ubi8/nodejs-16:1 as builder

#USER 1001

# Add application sources
#COPY --chown=1001:1001 . $HOME
COPY . $HOME

# Install the dependencies
RUN npm install @angular/cli@12.2.8 && \
    npm run build && \
    npm prune --production

# Second stage copies the application to the minimal image
FROM registry.access.redhat.com/ubi8/nodejs-16-minimal:1

# ENV variables
# API_BASE_URL: URL of service to connect to
# HTTP_PORT: The http port this service listens on
ENV API_BASE_URL=http://localhost:8082 \
    HTTP_PORT=8080 \
    NODE_ENV=production

#USER 1001

# Copy the application source and build artifacts from the builder image to this one
#COPY --chown=1001:1001 --from=builder $HOME $HOME
COPY --from=builder $HOME $HOME

# Expose the http port
EXPOSE $HTTP_PORT

# Run script uses standard ways to run the application
CMD npm start
