'use strict'

const { get } = require('env-var')
const { join } = require('path')

module.exports = {
    HTTP_PORT: get('HTTP_PORT').default(8080).asPortNumber(),

    API_BASE_URL: get('API_BASE_URL').default('http://localhost:8082').asUrlString(),

    IS_KUBERNETES: get('IS_KUBERNETES').default("false").asBool(),

    // Location of Angular build and other files. Defaults to the dist/
    // in the root of the project
    STATIC_DIR: get('STATIC_DIR').default(join(__dirname, '../dist/super-heroes')).asString()
}
