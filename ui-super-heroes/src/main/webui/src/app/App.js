import {FightList} from "./fight-list/FightList"
import Fight from "./fight/Fight"
import {useEffect, useState} from "react"
import {getFights} from "./shared/api/fight-service"
import Login from "./auth/Login2"
function App() {
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
  const [fights, setFights] = useState()
  const [isLoggedIn, setIsLoggedIn] = useState(false)


  const refreshFights = () => getFights().then(answer => setFights(answer))

  useEffect(() => {
      refreshFights()
    }, []
  )

  useEffect(() => {
    fetch(`${basePath}` + '/check-cookie', {
      credentials: 'include',
      method: 'GET'
      
    })
    .then(response => {
      if(response.ok) {
        setIsLoggedIn(true); 
      } else {
        setIsLoggedIn(false);
      }
    })
    .catch(error => {
      console.error('Session check failed:', error);
      setIsLoggedIn(false); 
    });
  }, []); 
  
  const handleLoginSuccess = () => {
    setIsLoggedIn(true);
    refreshFights(); // Optionally refresh fights or perform other actions post login
  };

  return (
    <>
    {isLoggedIn ? (
      <>
        <h1>Welcome to Super Heroes Fight!</h1>
        {/* Uncomment these if they should also wait for login
            <Fight onFight={refreshFights}/>
            <FightList fights={fights}/>
        */}
      </>
    ) : (
      <Login onLoginSuccess={handleLoginSuccess} /> // Passing the callback to Login component
    )}
  </>


    // {/* <>
    //   <h1>
    //     Welcome to Super Heroes Fight!
    //   </h1>
    //   <Login />
    //   {/* <Fight onFight={refreshFights}/> */}
    //   {/* <FightList fights={fights}/> */}
    // </> */}
  )
}

export default App
