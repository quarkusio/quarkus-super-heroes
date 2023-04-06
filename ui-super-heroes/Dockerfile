# First stage builds the application
FROM registry.access.redhat.com/ubi9/nodejs-18:1-26.1675794538 as builder

USER 1001

# This is a hack, but until the UI has been completely re-written, this will have to do
ENV NODE_OPTIONS=--openssl-legacy-provider

# Add dependencies
COPY --chown=1001:1001 package*.json $HOME/

# Install dependencies
RUN npm install

# Add application sources
COPY --chown=1001:1001 . $HOME/

# Run build
RUN npm run build && \
    npm prune --production

# Second stage copies the application to the minimal image
FROM registry.access.redhat.com/ubi9/nodejs-18-minimal:1-27.1675790153

# ENV variables
# HTTP_PORT: The http port this service listens on
# NODE_OPTIONS=--openssl-legacy-provider is a hack, but until the UI has been completely re-written, this will have to do
ENV HTTP_PORT=8080 \
    NODE_ENV=production \
    NODE_OPTIONS=--openssl-legacy-provider

# Copy the application source and build artifacts from the builder image to this one
COPY --chown=1001:1001 --from=builder $HOME $HOME

# Expose the http port
EXPOSE $HTTP_PORT

# Run script uses standard ways to run the application
CMD npm start
