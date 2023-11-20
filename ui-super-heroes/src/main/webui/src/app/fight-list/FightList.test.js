import React from "react"
import {render, screen, within} from "@testing-library/react"
import "@testing-library/jest-dom"
import {FightList} from "./FightList"
import {act} from "react-dom/test-utils"

jest.mock("../shared/api/fight-service")

const fight = {
  fightDate: "2023-10-24T21:34:47.617598Z",
  id: 200,
  loserLevel: 1,
  loserName: "Fake hero",
  loserPicture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Mock+Hero",
  loserPowers: "Being fake",
  loserTeam: "heroes",
  winnerLevel: 42,
  winnerName: "Fake villain",
  winnerPicture: "https://dummyimage.com/280x380/b22222/ffffff&text=Mock+Villain",
  winnerPowers: "Dissimulation",
  winnerTeam: "villains",
  location: {
    name: "Gotham City",
    picture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Gotham"
  }
}

describe("the fight list", () => {
  it("handles missing fights gracefully", async () => {
    await act(async () => {
      render(<FightList/>)
    })

    // We don't care too much if it renders headings or shows blank, we just want there not to be an error
  })

  it("renders a table with column headings", async () => {
    await act(async () => {
      render(<FightList fights={[fight]}/>)
    })

    const table = screen.getByRole("table")
    expect(table).toBeInTheDocument()

    const thead = within(table).getAllByRole('rowgroup')[0]
    const headRows = within(thead).getAllByRole("row")
    const headCols = within(headRows[0]).getAllByRole("columnheader")

    expect(headCols).toHaveLength(5)
    expect(headCols[0]).toHaveTextContent("Id")
    expect(headCols[1]).toHaveTextContent("Fight Date")
    expect(headCols[2]).toHaveTextContent("Winner")
    expect(headCols[3]).toHaveTextContent("Loser")
    expect(headCols[4]).toHaveTextContent("Location")
  })

  it("renders rows for the fights", async () => {
    await act(async () => {
      render(<FightList fights={[fight]}/>)
    })

    const table = screen.getByRole("table")
    const tbody = within(table).getAllByRole('rowgroup')[1];
    const bodyRows = within(tbody).getAllByRole('row');
    const rowCols = within(bodyRows[0]).getAllByRole("cell")

    expect(rowCols).toHaveLength(5)
    expect(rowCols[0]).toHaveTextContent(fight.id)
    expect(rowCols[1]).toHaveTextContent(fight.fightDate)
    expect(rowCols[2]).toHaveTextContent(fight.winnerName)
    expect(rowCols[3]).toHaveTextContent(fight.loserName)
    expect(rowCols[4]).toHaveTextContent(fight.location.name)
    expect(screen.getByText(fight.location.name).closest("a")).toHaveAttribute("href", fight.location.picture)
  })
})
