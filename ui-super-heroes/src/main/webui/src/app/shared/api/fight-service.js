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
 * Returns all the fights from the database
 *
 */
export async function getFights() {

  const response = await axios.get(`${basePath}/api/fights`,
    {
      headers: defaultHeaders,
    }
  )

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
      responseType: 'text',
      headers,
    }
  )

  return response.data
}
