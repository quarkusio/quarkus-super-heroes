/**
 * Fight API
 * This API allows a hero and a villain to fight
 */


import axios from "axios"

const defaultHeaders = {'Content-Type': "application/json"}

let basePath = window?.APP_CONFIG?.API_BASE_URL
const calculateApiBaseUrl = window?.APP_CONFIG?.CALCULATE_API_BASE_URL

if (calculateApiBaseUrl) {
  // If calculateApiBaseUrl then just replace "ui-super-heroes" with "rest-fights" in the current URL
  basePath = window.location.protocol + "//" + window.location.host.replace('ui-super-heroes', 'rest-fights')
}

// Fallback to whatever is in the browser if basePath isn't set
if (!basePath) {
  basePath = window.location.protocol + "//" + window.location.host
}


/**
 * Returns fights from the API. Call with no arguments to load every fight (matches backend when no query params).
 * Pass `{ page, size }` to request a paginated slice (newest first).
 *
 * @param [options] Optional `{ page, size }` for pagination
 */
export async function getFights(options = {}) {
  const { page, size } = options
  const config = { headers: defaultHeaders }
  if (page !== undefined || size !== undefined) {
    config.params = {
      page: page ?? 0,
      size: size ?? 20,
    }
  }

  const response = await axios.get(`${basePath}/api/fights`, config)

  return response.data
}

/**
 * Creates a fight between two fighters
 *
 * @param body The two fighters fighting
 */
export async function startFight(body) {

  if (body === null || body === undefined) {
    throw new Error('Required parameter body was null or undefined when calling startFight.')
  }

  const response = await axios.post(
    `${basePath}/api/fights`, body, {
      crossDomain: true,
      defaultHeaders,
    }
  )
  return response.data
}

/**
 * Returns a random location
 */
export async function getRandomLocation() {
  try {
    const response = await axios.get(`${basePath}/api/fights/randomlocation`,
        {
            headers: defaultHeaders,
        })
    return response.data
  }
  catch (error) {
    console.error(error)
  }
}

/**
 * Returns two random fighters
 */
export async function getRandomFighters() {


  // Explicitly catch errors and return undefined, since this API is used first
  try {
    const response = await axios.get(`${basePath}/api/fights/randomfighters`,
      {
        headers: defaultHeaders,
      }
    )
    return response.data
  } catch (error) {
    console.error(error)
  }
}

/**
 * Generations a narration, given a pre-built fight
 *
 * @param body a fight
 */
export async function narrateFight(body) {

  if (body === null || body === undefined) {
    throw new Error('Required parameter body was null or undefined when calling narrateFight.')
  }

  const headers = {...defaultHeaders, 'Accept': 'text/plain'}

  const response = await axios.post(`${basePath}/api/fights/narrate`,
    body,
    {
      crossDomain: true,
      responseType: 'text',
      headers,
    }
  )

  return response.data
}

/**
 * Generates an image from a narration
 *
 * @param narration the narration
 * @param winnerPictureUrl the winner's picture URL
 * @param loserPictureUrl the loser's picture URL
 */
export async function generateImage(narration, winnerPictureUrl, loserPictureUrl) {

  if (narration === null || narration === undefined) {
    throw new Error('Required parameter narration was null or undefined when calling generateImage.')
  }

  const headers = {...defaultHeaders, 'Accept': 'application/json'}

  const response = await axios.post(`${basePath}/api/fights/narrate/image`,
    { narration, winnerPictureUrl, loserPictureUrl },
    {
      crossDomain: true,
      headers,
      timeout: 120000,
    }
  )

  return response.data
}
