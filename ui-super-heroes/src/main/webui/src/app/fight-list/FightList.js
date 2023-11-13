import {useEffect, useState} from "react"
import {getFights} from "../shared/api/fight-service"

export function FightList() {


  const [fights, setFights] = useState()

  useEffect(() => {
      getFights().then(answer => setFights(answer))
    }, []
  )

  return (
    <table className="table table-striped" data-testid="fights-list">
      <thead>
      <tr>
        <th className="fight-list-header thead-dark">Id</th>
        <th className="fight-list-header thead-dark">Fight Date</th>
        <th className="fight-list-header thead-dark">Winner</th>
        <th className="fight-list-header thead-dark">Loser</th>
        <th className="fight-list-header thead-dark">Location</th>
      </tr>
      </thead>
      <tbody>


      {fights && fights.map(element => (
        <tr key={element.id}>
          <td className="fight-list-cell"> {element.id} </td>
          <td className="fight-list-cell"> {element.fightDate} </td>
          <td className="fight-list-cell"> {element.winnerName} </td>
          <td className="fight-list-cell"> {element.loserName} </td>
          <td className="fight-list-cell"><a href={element?.location?.picture}>{element?.location?.name}</a></td>
        </tr>))}
      </tbody>
    </table>
  )
}

