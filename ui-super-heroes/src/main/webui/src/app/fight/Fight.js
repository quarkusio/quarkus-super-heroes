import {getRandomFighters, getRandomLocation, narrateFight, startFight} from "../shared/api/fight-service"
import {useEffect, useState} from "react"
import {faComment} from "@fortawesome/free-solid-svg-icons"
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome"
import {trackPromise} from "react-promise-tracker";

function Fight({onFight}) {
  const [fighters, setFighters] = useState()
  const [fightResult, setFightResult] = useState()
  const [narration, setNarration] = useState()
  const [location, setLocation] = useState()

  const newFighters = () => {
    trackPromise(
        getRandomFighters().then(answer => {
          setFighters(answer)
          clearPreviousFight()
        })
    )
  }

  const narrate = () => {
    trackPromise(
        narrateFight(fightResult).then(answer => setNarration(answer))
    )
  }

  const newLocation = () => {
    trackPromise(
        getRandomLocation().then(answer => {
          setLocation(answer)
          clearPreviousFight()
        })
    )
  }

  const clearPreviousFight = () => {
    setNarration(undefined)
    setFightResult(undefined)
  }

  const fight = () => {
    // Create a fight to perform by combining the fighters with the location
    const fightToPerform = fighters
    fightToPerform.location = location

    trackPromise(
        startFight(fightToPerform).then(response => {
          setFightResult(response)
          onFight()
        })
    )
  }

  // This initialises the component on its initial load with a call to get fighters
  useEffect(newFighters, [])
  useEffect(newLocation, [])

  const winner = fightResult?.winnerName
  const heroWinnerCss = (winner === fighters?.hero?.name) ? 'hero-winner-card' : 'off'
  const villainWinnerCss = (winner === fighters?.villain?.name) ? 'villain-winner-card' : 'off'

  if (!fighters) {
    return (
      <div>No back-end is available. Do you need to start some services?</div>
    )
  } else
    return (
      <div id="fight-row">
        <div className="character flip-card">
          <div className={heroWinnerCss}>
            <h2 className="hero-name">
              {fighters?.hero?.name}<br/>
              <span style={{fontSize: "small"}}>(Hover over for more info)</span>
            </h2>
            <div className="card-pf-body flip-card-inner">
              <div className={heroWinnerCss + ' card-pf-body flip-card-front'}>
                <img className="rounded" src={fighters?.hero?.picture} alt="the hero"/>
              </div>
              <div className={heroWinnerCss + ' card-pf-body flip-card-back'}>
                <h4><strong>Hero Details</strong></h4>
                <table>
                  <tbody>
                  <tr>
                    <td className="flipcard-row-header">Level:</td>
                    <td className="flipcard-row-value">{fighters?.hero?.level}</td>
                  </tr>
                  <tr>
                    <td className="flipcard-row-header">Powers:</td>
                    <td className="flipcard-row-value">{fighters?.hero?.powers}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div className="card-pf-body space-eater">
              {/* This div is a major hack just to take up visible space since the cards use absolute positioning */}
              <img className="rounded" src={fighters?.hero?.picture} alt="the hero"/>
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
              <div className="flip-card">
                <div className="flip-card-inner">
                  <div className="flip-card-front">
                    <div className="narration-text"><strong><span data-testid="location-name">{location.name}: </span></strong>{location.description}</div>
                    <div><img alt="Location" className="squared" src={location.picture}></img></div>
                  </div>
                  <div className="flip-card-back">
                    <h4><strong>Location Details</strong></h4>
                    <table>
                      <tbody>
                      <tr>
                        <td className="flipcard-row-header">Name:</td>
                        <td className="flipcard-row-value">{location.name}</td>
                      </tr>
                      <tr>
                        <td className="flipcard-row-header">Description:</td>
                        <td className="flipcard-row-value">{location.description}</td>
                      </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
              )}
              {location && (
                  <div className="space-eater">
                    {/* This div is a major hack just to take up visible space since the cards use absolute positioning */}
                    <div className="narration-text"><strong><span>{location.name}: </span></strong>{location.description}</div>
                    <div><img className="squared" src={location.picture}></img></div>
                  </div>
              )}
              <button onClick={fight} className="btn btn-danger btn-block btn-lg">
                <h4><i className="fab fa-battle-net"></i> FIGHT !</h4>
              </button>
            </div>

            {winner && (<div className="winner-text">
                Winner is <span
                className={winner === fighters.villain.name ? 'winner-villain' : 'winner-hero'}>{winner}</span>!
                <button onClick={narrate} className="btn btn-secondary btn-block btn-lg">
                  <h4><FontAwesomeIcon icon={faComment}/> NARRATE THE FIGHT
                  </h4>
                </button>
                {narration && (<div className="narration-text">{narration}</div>)}
              </div>
            )}
          </div>
        </div>

        <div className="character flip-card">
          <div className={villainWinnerCss}>
            <h2 className="villain-name">
              {fighters?.villain?.name}<br/>
              <span style={{fontSize: "small"}}>(Hover over for more info)</span>
            </h2>
            <div className="flip-card-inner">
              <div className={villainWinnerCss + ' card-pf-body flip-card-front'}>
                <img className="rounded" src={fighters?.villain?.picture} alt="the villain"/>
              </div>
              <div className={villainWinnerCss + ' card-pf-body flip-card-back'}>
                <h4><strong>Villain Details</strong></h4>
                <table>
                  <tbody>
                  <tr>
                    <td className="flipcard-row-header">Level:</td>
                    <td className="flipcard-row-value">{fighters?.villain?.level}</td>
                  </tr>
                  <tr>
                    <td className="flipcard-row-header">Powers:</td>
                    <td className="flipcard-row-value">{fighters?.villain?.powers}</td>
                  </tr>
                  </tbody>
                </table>
              </div>
            </div>
            <div className="card-pf-body space-eater">
              {/* This div is a major hack just to take up visible space since the cards use absolute positioning */}
              <img className="rounded" src={fighters?.villain?.picture} alt="the villain"/>
            </div>
          </div>
        </div>
      </div>
    )
}

export default Fight
