'use strict'

const express = require('express')
const log = require('barelog');
const { join } = require('path');
const { HTTP_PORT, API_BASE_URL, CALCULATE_API_BASE_URL, STATIC_DIR } = require('./config')

const app = express();

// Serve requests for the /env.js environment configuration
app.get('/env.js', (req, res) => {
  res.setHeader('Content-Type', 'application/javascript');

  let ngConfig = {};

  if (typeof API_BASE_URL !== 'undefined') {
      ngConfig['API_BASE_URL'] = API_BASE_URL;
  }

  if (typeof CALCULATE_API_BASE_URL !== 'undefined') {
      ngConfig['CALCULATE_API_BASE_URL'] = CALCULATE_API_BASE_URL
  }

  res.send('window.NG_CONFIG=' + JSON.stringify(ngConfig));
})

// Serve files from the configured static directory
app.use(express.static(STATIC_DIR))

// Return the index.html for /
app.get('/', (req, res) => res.sendFile(join(STATIC_DIR, 'index.html')))

app.listen(HTTP_PORT, '0.0.0.0', () => {
  log(`express server listening on ${HTTP_PORT}`)
})
