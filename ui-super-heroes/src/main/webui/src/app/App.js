import {FightList} from "./fight-list/FightList"
import Fight from "./fight/Fight"
import {useEffect, useState} from "react"
import {getFights} from "./shared/api/fight-service"
import Login from "./auth/Login"
function App() {

  let authPath = window?.APP_CONFIG?.AUTH_URL


// Fallback to whatever is in the browser if basePath isn't set

  const [fights, setFights] = useState()
  const [isLoggedIn, setIsLoggedIn] = useState(false)


  const refreshFights = () => getFights().then(answer => setFights(answer))

  useEffect(() => {
      refreshFights()
    }, []
  )

  useEffect(() => {
    fetch(`${authPath}` + '/verify-session', {
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
        <Fight onFight={refreshFights}/>
        <FightList fights={fights}/>
       
      </>
    ) : (
      <Login onLoginSuccess={handleLoginSuccess} /> // Passing the callback to Login component
    )}
  </>
  )
}

export default App
