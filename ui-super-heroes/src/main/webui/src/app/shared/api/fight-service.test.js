import {getFights, getRandomFighters, getRandomLocation, narrateFight, startFight} from "./fight-service"
import axios from "axios"

jest.mock('axios')

const fightersData = {
  hero: {
    name: 'Fallback hero',
    level: 1,
    picture: 'https://dummyimage.com/280x380/1e8fff/ffffff&text=Fallback+Hero',
    powers: 'Fallback hero powers'
  },
  villain: {
    name: 'Fallback villain',
    level: 42,
    picture: 'https://dummyimage.com/280x380/b22222/ffffff&text=Fallback+Villain',
    powers: 'Fallback villain powers'
  }
}

const fightersResponse = {
  data: fightersData
  ,
  status: 200,
  statusText: 'OK',
  headers: {
    'content-type': 'application/json;charset=UTF-8',
    'content-length': '314'
  },
  config: {
    transitional: {
      silentJSONParsing: true,
      forcedJSONParsing: true,
      clarifyTimeoutError: false
    },
    adapter: ['xhr', 'http'],

    headers: {
      Accept: 'application/json, text/plain, */*',
      'Content-Type': null
    },
    method: 'get',
    url: 'http://localhost:8082/api/fights/randomfighters',
    data: undefined
  },
}

const locationData = {
  name: "Mock location",
  picture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Mock",
  description: "Mock location description"
}

const locationResponse = {
  data: locationData,
  headers: {"content-length": "433", "content-type": "application/json;charset=UTF-8"}
}

const fightData = {
  fightDate: "2023-10-24T21:34:47.617598Z",
  id: 200,
  loserLevel: 1,
  loserName: "Mock hero",
  loserPicture: "https://dummyimage.com/280x380/1e8fff/ffffff&text=Mock+Hero",
  loserPowers: "Being fake",
  loserTeam: "heroes",
  winnerLevel: 42,
  winnerName: "Mock villain",
  winnerPicture: "https://dummyimage.com/280x380/b22222/ffffff&text=Mock+Villain",
  winnerPowers: "Dissimulation",
  winnerTeam: "villains",
  location: locationData
}

const fightResponse = {
  data: fightData,
  headers: {"content-length": "433", "content-type": "application/json;charset=UTF-8"}
}

const fightsList = {
  data: [fightData],
  headers: {"content-length": "433", "content-type": "application/json;charset=UTF-8"}
}

const narrationData = "It was a dark and stormy night"
const narrationResponse = {
  data: narrationData,
  headers: {"content-length": "433", "content-type": "text/plain"}
}


describe("the fight service", () => {

  describe("getting random fighters", () => {

    beforeEach(() => {
      axios.get.mockResolvedValue(fightersResponse)
      jest.spyOn(console, 'error')
      console.error.mockImplementation(() => null)
    })

    afterEach(() => {
      jest.resetAllMocks()
      console.error.mockRestore()
    })

    it("invokes the remote api", async () => {
      await getRandomFighters({})
      expect(axios.get).toHaveBeenCalled()
    })

    it("returns fighters", async () => {
      const answer = await getRandomFighters({})
      expect(answer).toStrictEqual(fightersData)
    })

    describe("when back-end services are missing", () => {
      beforeEach(() => {
        axios.get.mockRejectedValue(new Error('Deliberate error: No Java services available'))
      })

      afterEach(() => {
        jest.resetAllMocks()
      })

      it("gets new fighters", async () => {
        const answer = await getRandomFighters({})
        expect(answer).toBeUndefined()
      })
    })
  })

  describe("getting random location", () => {

    beforeEach(() => {
      axios.get.mockResolvedValue(locationResponse)
    })

    afterEach(() => {
      jest.resetAllMocks()
    })

    it("invokes the remote api", async () => {
      await getRandomLocation({})
      expect(axios.get).toHaveBeenCalled()
    })

    it("returns location", async () => {
      const answer = await getRandomLocation({})
      expect(answer).toStrictEqual(locationData)
    })

    describe("when back-end services are missing", () => {
      beforeEach(() => {
        axios.get.mockRejectedValue(new Error('Deliberate error: No Java services available'))
        jest.spyOn(console, 'error')
        console.error.mockImplementation(() => null)
      })

      afterEach(() => {
        jest.resetAllMocks()
        console.error.mockRestore()
      })

      it("gets new location", async () => {
        const answer = await getRandomLocation({})
        expect(answer).toBeUndefined()
      })
    })
  })

  describe("triggering a fight", () => {
    beforeEach(() => {
      axios.post.mockResolvedValue(fightResponse)
      jest.spyOn(console, 'error')
      console.error.mockImplementation(() => null)
    })

    afterEach(() => {
      jest.resetAllMocks()
      console.error.mockRestore()
    })

    it("invokes the remote api", async () => {
      const response = await startFight(fightersData)
      expect(axios.post).toHaveBeenCalled()
      expect(axios.post).toHaveBeenLastCalledWith(expect.anything(), fightersData, expect.anything())
    })

    it("returns the data", async () => {
      const answer = await startFight(fightersData)
      expect(answer).toStrictEqual(fightData)
    })
  })

  describe("narration", () => {

    beforeEach(() => {
      axios.post.mockResolvedValue(narrationResponse)
      jest.spyOn(console, 'error')
      console.error.mockImplementation(() => null)
    })

    afterEach(() => {
      jest.resetAllMocks()
      console.error.mockRestore()
    })

    it("invokes the remote api", async () => {
      await narrateFight(fightData)
      expect(axios.post).toHaveBeenCalled()
      expect(axios.post).toHaveBeenLastCalledWith(expect.anything(), fightData, expect.anything())
    })

    it("narrates the fight", async () => {
      const answer = await narrateFight(fightData)
      expect(answer).toStrictEqual(narrationData)
    })
  })

  describe("listing fights", () => {
    beforeEach(() => {
      axios.get.mockResolvedValue(fightsList)
      jest.spyOn(console, 'error')
      console.error.mockImplementation(() => null)
    })

    afterEach(() => {
      jest.resetAllMocks()
      console.error.mockRestore()
    })

    it("invokes the remote api", async () => {
      const response = await getFights(fightersData)
      expect(axios.get).toHaveBeenCalled()
    })

    it("returns the data", async () => {
      const answer = await getFights(fightersData)
      expect(answer).toStrictEqual([fightData])
    })
  })
})
