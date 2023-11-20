import {getRandomFighters, getRandomLocation, narrateFight, startFight} from "../shared/api/fight-service"
import {useEffect, useState} from "react"
import {faComment} from "@fortawesome/free-solid-svg-icons"
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome"


function Fight({onFight}) {
  const [fighters, setFighters] = useState()
  const [fightResult, setFightResult] = useState()
  const [narration, setNarration] = useState()
  const [location, setLocation] = useState()
  const [showVillainPowers, setShowVillainPowers] = useState()
  const [showHeroPowers, setShowHeroPowers] = useState()

  const newFighters = () => {
    getRandomFighters().then(answer => setFighters(answer))
  }

  const narrate = () => {
    narrateFight(fightResult).then(answer => setNarration(answer))
  }

  const newLocation = () => {
    getRandomLocation().then(answer => setLocation(answer))
  }

  const fight = () => {
    // Create a fight to perform by combining the fighters with the location
    const fightToPerform = fighters
    fightToPerform.location = location

    startFight(fightToPerform).then(response => {
      setFightResult(response)
      onFight()
    })
  }

  // This initialises the component on its initial load with a call to get fighters
  useEffect(newFighters, [])
  useEffect(newLocation, [])

  const winner = fightResult?.winnerName

  if (!fighters) {
    return (
      <div>No back-end is available. Do you need to start some services?</div>
    )
  } else
    return (
      <div className="row" id="fight-row">
        <div>
          <div className={winner === fighters.hero.name ? 'hero-winner-card' : 'off'}>
            <h2 className="hero-name">
              {fighters.hero.name}
            </h2>
            <div className="card-pf-body">
              <img className="rounded" src={fighters.hero.picture} alt="the hero"/>

              <h2><i className="fas fa-bolt"></i> {fighters.hero.level}</h2>
              <h2><a data-toggle="collapse" href="#heroPowers" role="button" aria-expanded="false"
                     aria-controls="heroPowers" onClick={() => setShowHeroPowers(!showHeroPowers)}><i
                className="powers hero fas fa-atom"></i></a></h2>

              <div className={showHeroPowers ? "" : "collapse"} id="heroPowers">
                {fighters.hero.powers}
              </div>
            </div>
          </div>
        </div>

        <div className="controls">
          <div className="card-pf">
            <div className="card-pf-body">
              <button onClick={newFighters} className="btn btn-primary btn-block btn-lg">
                <h4><i className="fas fa-random"></i> NEW FIGHTERS</h4>
              </button>
              <button onClick={newLocation} className="btn btn-primary btn-block btn-lg">
                <h4><i className="fas fa-random"></i> NEW LOCATION </h4>
              </button>
              {location && (
                <div className="narration-text"><strong><span data-testid="location-name">{location.name}: </span></strong>{location.description}</div>
              )}
              {location && (
                <div><img alt="Location" className="squared" src={location.picture}></img></div>
              )}
              <button onClick={fight} className="btn btn-danger btn-block btn-lg">
                <h4><i className="fab fa-battle-net"></i> FIGHT !</h4>
              </button>
            </div>

            {winner && (<div className="winner-text">
                Winner is <span
                className={winner === fighters.villain.name ? 'winner-villain' : 'winner-hero'}>{winner}</span>
                <button onClick={narrate} className="btn btn-secondary btn-block btn-lg">
                  <h4><FontAwesomeIcon icon={faComment}/> NARRATE THE FIGHT
                  </h4>
                </button>
                {narration && (<div className="narration-text">{narration}</div>)}
              </div>
            )}
          </div>
        </div>

        <div>
          <div className={winner === fighters.villain.name ? 'villain-winner-card' : 'off'}>
            <h2 className="villain-name">
              {fighters.villain.name}
            </h2>
            <div className="card-pf-body">
              <img className="rounded" src={fighters.villain.picture} alt="the villain"/>

              <h2><i className="fas fa-bolt"></i> {fighters.villain.level}</h2>
              <h2><a href="#villainPowers" role="button" aria-expanded="false"
                     aria-controls="villainPowers" onClick={() => setShowVillainPowers(!showVillainPowers)}><i
                className="powers villain fas fa-atom"></i></a></h2>

              <div className={showVillainPowers ? "" : "collapse"} id="villainPowers">
                {fighters.villain.powers}
              </div>

            </div>
          </div>
        </div>

      </div>
    )
}

export default Fight
