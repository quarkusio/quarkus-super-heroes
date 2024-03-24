import {useEffect, useState} from "react"

const Hide = ({hide,children}) => (hide ? <></> : <>{children}</>)
export default Hide