import React from "react";
import {render, screen, within} from "@testing-library/react";
import App from "./App";
import "@testing-library/jest-dom";
import {act} from "react-dom/test-utils";
import {getFights, getRandomFighters, getRandomLocation} from "./shared/api/fight-service";

jest.mock("./shared/api/fight-service")

const fighters = {
  hero: {
    name: 'Fake hero',
    level: 1,
    picture: 'https://dummyimage.com/280x380/1e8fff/ffffff&text=Fake+Hero',
    powers: 'Fake hero powers'
  },
  villain: {
    name: 'Fake villain',
    level: 42,
    picture: 'https://dummyimage.com/280x380/b22222/ffffff&text=Fake+Villain',
    powers: 'Fake villain powers'
  }
}

const location = {
  name: "Gotham City",
  picture: 'https://dummyimage.com/280x380/b22222/ffffff&text=Gotham',
  description: "This is Gotham City"
}

const fight = {
  fightDate: "2023-10-24T21:34:47.617598Z",
  id: 200,
  loserLevel: 1,
  loserName: "Some hero",
  loserPicture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Mock+Hero",
  loserPowers: "Being fake",
  loserTeam: "heroes",
  winnerLevel: 42,
  winnerName: "Some villain",
  winnerPicture: "https://dummyimage.com/280x380/b22222/ffffff&text=Mock+Villain",
  winnerPowers: "Dissimulation",
  winnerTeam: "villains",
  location: location
}

describe("renders the elements", () => {
  beforeEach(() => {
    getRandomFighters.mockResolvedValue(fighters)
    getRandomLocation.mockResolvedValue(location)
    getFights.mockResolvedValue([fight])
  })

  afterAll(() => {
    jest.resetAllMocks()
  })

  it('renders a suitable title', async () => {
    await act(async () => {
      render(<App />);
    })

    expect(screen.getByText(/Super Heroes/i)).toBeInTheDocument();
  });

  it("renders a table with column headings", async () => {
    await act(async () => {
      render(<App/>)
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

    const tbody = within(table).getAllByRole('rowgroup')[1];
    const bodyRows = within(tbody).getAllByRole('row');
    const rowCols = within(bodyRows[0]).getAllByRole("cell")

    expect(rowCols).toHaveLength(5)
    expect(rowCols[0]).toHaveTextContent(fight.id)
    expect(rowCols[1]).toHaveTextContent(fight.fightDate)
    expect(rowCols[2]).toHaveTextContent(fight.winnerName)
    expect(rowCols[3]).toHaveTextContent(fight.loserName)
    expect(rowCols[4]).toHaveTextContent(fight.location.name)
  })

  it("renders fighters", async () => {
    await act(async () => {
      render(<App/>)
    })
    expect(screen.getByText(fighters.hero.name)).toBeInTheDocument()
    expect(screen.getByText(fighters.villain.name)).toBeInTheDocument()
    expect(screen.getByText(/NEW FIGHTERS/i)).toBeInTheDocument()
    expect(screen.getByText(/NEW LOCATION/i)).toBeInTheDocument()
    expect(screen.getByText(/FIGHT !/i)).toBeInTheDocument()
  })
})
