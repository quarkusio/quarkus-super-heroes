export function FightList({fights}) {
  return (
    <table className="fights-table table-striped" role="grid" aria-label="fights-table">
      <thead>
        <tr role="rowheader">
          <th role="columnheader" className="fight-list-header thead-dark">Id</th>
          <th role="columnheader" className="fight-list-header thead-dark">Fight Date</th>
          <th role="columnheader" className="fight-list-header thead-dark">Winner</th>
          <th role="columnheader" className="fight-list-header thead-dark">Loser</th>
          <th role="columnheader" className="fight-list-header thead-dark">Location</th>
        </tr>
      </thead>
      <tbody>

      {fights && fights.map(element => (
        <tr role="row" key={element.id}>
          <td role="cell" data-label="Id" className="fight-list-cell"> {element.id} </td>
          <td role="cell" data-label="Fight Date" className="fight-list-cell"> {element.fightDate} </td>
          <td role="cell" data-label="Winner" className="fight-list-cell"> {element.winnerName} </td>
          <td role="cell" data-label="Loser" className="fight-list-cell"> {element.loserName} </td>
          <td role="cell" data-label="Location" className="fight-list-cell"><a href={element?.location?.picture}>{element?.location?.name}</a></td>
        </tr>))}
      </tbody>
    </table>
  )
}

