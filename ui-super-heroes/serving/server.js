'use strict'

const express = require('express')
const log = require('barelog');
const { join } = require('path');
// const { writeFileSync } = require('fs');
const { HTTP_PORT, STATIC_DIR, API_BASE_URL } = require('./config')

const app = express();

// Generate the env.js file inside the dist folder
// writeFileSync(join(STATIC_DIR, 'env.js'), `window.NG_CONFIG={ API_BASE_URL: "${API_BASE_URL}" }`)
app.get('/env.js', (req, res) => {
    res.setHeader('Content-Type', 'application/javascript');
    res.send(`window.NG_CONFIG={ API_BASE_URL: "${API_BASE_URL}" }`);
})

// Serve files from the configured static directory
app.use(express.static(STATIC_DIR))

// Return the index.html for /
app.get('/', (req, res) => res.sendFile(join(STATIC_DIR, 'index.html')))

app.listen(HTTP_PORT, '0.0.0.0', () => {
  log(`express server listening on ${HTTP_PORT}`)
})
