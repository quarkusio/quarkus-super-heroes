import {FightList} from "./fight-list/FightList"
import Fight from "./fight/Fight"
import {useEffect, useState} from "react"
import {getFights} from "./shared/api/fight-service"

function App() {
  const [fights, setFights] = useState()
  const refreshFights = () => getFights().then(answer => setFights(answer))

  useEffect(() => {
      refreshFights()
    }, []
  )

  return (
    <>
      <h1>
        Welcome to Super Heroes Fight!
      </h1>
      <Fight onFight={refreshFights}/>
      <FightList fights={fights}/>
    </>
  )
}

export default App
