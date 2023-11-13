import React from "react"
import {fireEvent, render, screen} from "@testing-library/react"
import "@testing-library/jest-dom"
import Fight from "./Fight"
import {getRandomFighters, getRandomLocation, narrateFight, startFight} from "../shared/api/fight-service"
import {act} from "react-dom/test-utils"

jest.mock("../shared/api/fight-service")

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
  loserName: "Fake hero",
  loserPicture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Mock+Hero",
  loserPowers: "Being fake",
  loserTeam: "heroes",
  winnerLevel: 42,
  winnerName: "Fake villain",
  winnerPicture: "https://dummyimage.com/280x380/b22222/ffffff&text=Mock+Villain",
  winnerPowers: "Dissimulation",
  winnerTeam: "villains",
  location: location
}

const narration = "Ooh, it was a close fight but in the end the villain prevailed by sitting on the hero."

describe("the fight visualisation", () => {

  describe("when the back end is missing", () => {

    beforeEach(() => {
      // Return undefined
      getRandomFighters.mockResolvedValue()
      getRandomLocation.mockResolvedValue()
      jest.spyOn(console, 'error')
      console.error.mockImplementation(() => null)
    })

    afterEach(() => {
      console.error.mockRestore()
    })

    afterAll(() => {
      jest.resetAllMocks()
    })

    it("renders a helpful message", async () => {
      await act(async () => {
        render(<Fight/>)
      })
      expect(screen.getByText(/back-end/i)).toBeInTheDocument()
    })
  })

  describe("when a back end is available", () => {
    beforeEach(() => {
      getRandomFighters.mockResolvedValue(fighters)
      getRandomLocation.mockResolvedValue(location)
      startFight.mockResolvedValue(fight)
      narrateFight.mockResolvedValue(narration)
    })

    afterAll(() => {
      jest.resetAllMocks()
    })

    it("renders fighters", async () => {
      await act(async () => {
        render(<Fight/>)
      })
      expect(screen.getByText("Fake hero")).toBeInTheDocument()
      expect(screen.getByText("Fake villain")).toBeInTheDocument()
    })

    it("renders a fight button", async () => {
      await act(async () => {
        render(<Fight/>)
      })
      const button = screen.getByText(/FIGHT !/i)
      expect(button).toBeInTheDocument()
    })

    it("renders a get random location button", async() => {
      await act(async () => {
        render(<Fight/>)
      })
      const button = screen.getByText(/NEW LOCATION/i)
      expect(button).toBeInTheDocument()
    })

    it("renders winners when the fight button is clicked", async () => {
      await act(async () => {
        render(<Fight/>)
      })

      const nameCount = screen.getAllByText("Fake villain").length

      await act(async () => {
        fireEvent.click(screen.getByText(/FIGHT !/i))
      })
      expect(startFight).toHaveBeenLastCalledWith(fighters)
      expect(screen.getByText(/Winner is/i)).toBeInTheDocument()
      // The winner name is in a span by itself but there should be more occurrences of the name count
      expect(screen.getAllByText("Fake villain")).toHaveLength(nameCount + 1)
    })

    it("renders location when the new location button is clicked", async () => {
      await act(async () => {
        render(<Fight/>)
      })

      await act(async() => {
        fireEvent.click(screen.getByText(/NEW LOCATION/i))
      })

      expect(screen.queryByTestId("location-name").innerHTML).toEqual(location.name + ": ")
      expect(screen.getByAltText("Location")).toHaveAttribute("src", location.picture)
      expect(screen.getByText(location.description)).toBeInTheDocument()
    })

    it("renders narration when the narrate button is clicked", async () => {
      await act(async () => {
        render(<Fight/>)
      })

      const nameCount = screen.getAllByText("Fake villain").length

      await act(async () => {
        fireEvent.click(screen.getByText(/FIGHT !/i))
      })
      expect(screen.queryByText(narration)).not.toBeInTheDocument()
      await act(async () => {
        fireEvent.click(screen.getByText(/Narrate/i))
      })
      expect(narrateFight).toHaveBeenLastCalledWith(fight)
      expect(screen.getByText(narration)).toBeInTheDocument()
    })


  })

})
