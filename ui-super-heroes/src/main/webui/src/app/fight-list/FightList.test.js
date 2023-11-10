import React from "react"
import {render, screen} from "@testing-library/react"
import "@testing-library/jest-dom"
import {FightList} from "./FightList"
import {getFights} from "../shared/api/fight-service"
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
  beforeEach(() => {
    getFights.mockResolvedValue([fight])
  })

  afterAll(() => {
    jest.resetAllMocks()
  })


  it("renders a table with column headings", async () => {
    await act(async () => {
      render(<FightList/>)
    })
    expect(screen.getByText(/Winner/i)).toBeInTheDocument()
    expect(screen.getByText(/Loser/i)).toBeInTheDocument()
    expect(screen.getByText(/Fight Date/i)).toBeInTheDocument()
    expect(screen.getByText(/Location/i)).toBeInTheDocument()
  })

  it("renders rows for the fights", async () => {
    await act(async () => {
      render(<FightList/>)
    })

    expect(screen.getByText("Fake hero")).toBeInTheDocument()
    expect(screen.getByText("Fake villain")).toBeInTheDocument()
    expect(screen.getByText("2023-10-24T21:34:47.617598Z")).toBeInTheDocument()
    expect(screen.getByText("Gotham City")).toBeInTheDocument()
    expect(screen.getByText("Gotham City").closest("a")).toHaveAttribute("href", "https://dummyimage.com/280x380/1e8fff/ffffff&text=Gotham")
  })
})
