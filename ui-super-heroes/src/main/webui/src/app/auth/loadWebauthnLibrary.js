
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
  //https://react.dev/reference/react-dom/components/script#usage won't need to do this when that gets merged to stable
const loadWebAuthn = (callback) => {  
  const existingScript = document.getElementById('webAuthn');  
  if (!existingScript) {
      const script = document.createElement('script');
      script.src = `${basePath}` + '/q/webauthn/webauthn.js';    
      script.id = 'webAuthn';
      document.body.appendChild(script);    
      script.onload = () => {
        if (callback) callback();
      };
    }  if (existingScript && callback) callback();
  };
  export default loadWebAuthn;
