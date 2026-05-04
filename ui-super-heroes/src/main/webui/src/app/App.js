import {FightList} from "./fight-list/FightList"
import Fight from "./fight/Fight"
import {useCallback, useEffect, useState} from "react"
import {getFights} from "./shared/api/fight-service"

const PAGE_SIZE = 20

function App() {
  const [fights, setFights] = useState([])
  const [page, setPage] = useState(0)

  const loadFights = useCallback(async (nextPage) => {
    const data = await getFights({page: nextPage, size: PAGE_SIZE})
    setFights(Array.isArray(data) ? data : [])
  }, [])

  useEffect(() => {
    loadFights(page)
  }, [page, loadFights])

  const refreshFights = useCallback(() => {
    if (page === 0) {
      return loadFights(0)
    }
    setPage(0)
  }, [page, loadFights])

  const canGoPrevious = page > 0
  const canGoNext = fights.length === PAGE_SIZE

  return (
    <>
      <h1>
        Welcome to Super Heroes Fight!
      </h1>
      <Fight onFight={refreshFights}/>
      <section className="fight-list-section" aria-label="Fight history">
        <h2 className="fight-list-heading">Recent fights</h2>
        <p className="fight-list-subtitle">
          Newest first, {PAGE_SIZE} per page. Use Previous / Next to browse older fights.
        </p>
        <FightList fights={fights}/>
        <nav className="fight-pagination" aria-label="Fight list pagination">
          <button
            type="button"
            className="btn btn-secondary fight-pagination-btn"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={!canGoPrevious}
            aria-label="Previous page of fights"
          >
            Previous
          </button>
          <span className="fight-pagination-meta">
            Page {page + 1}
          </span>
          <button
            type="button"
            className="btn btn-secondary fight-pagination-btn"
            onClick={() => setPage((p) => p + 1)}
            disabled={!canGoNext}
            aria-label="Next page of fights"
          >
            Next
          </button>
        </nav>
      </section>
    </>
  )
}

export default App
