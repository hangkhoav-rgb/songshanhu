import { loadAMap } from './amap'

export interface LocationResult {
  lng: number
  lat: number
  accuracy?: number
  source: 'amap' | 'browser' | 'cache'
  timestamp: number
}

const CACHE_KEY = 'ssl_loc_cache'
const TTL_MS = 60_000

export async function getCachedLocation(): Promise<LocationResult | null> {
  try {
    const raw = localStorage.getItem(CACHE_KEY)
    if (!raw) return null
    const data = JSON.parse(raw) as LocationResult
    if (Date.now() - data.timestamp <= TTL_MS) return { ...data, source: 'cache' }
    return null
  } catch {
    return null
  }
}

export async function saveLocation(res: LocationResult) {
  localStorage.setItem(CACHE_KEY, JSON.stringify(res))
}

export async function getCurrentLocation(forceRefresh = false): Promise<LocationResult> {
  if (!forceRefresh) {
    const c = await getCachedLocation()
    if (c) return c
  }

  try {
    const AMap = await loadAMap()
    return await new Promise<LocationResult>((resolve, reject) => {
      AMap.plugin('AMap.Geolocation', function () {
        const geolocation = new AMap.Geolocation({
          enableHighAccuracy: true,
          timeout: 10000,
          position: 'RB',
          zoomToAccuracy: false
        })
        geolocation.getCurrentPosition((status: string, result: any) => {
          if (status === 'complete' && result && result.position) {
            const out: LocationResult = {
              lng: result.position.getLng(),
              lat: result.position.getLat(),
              accuracy: result.accuracy,
              source: 'amap',
              timestamp: Date.now()
            }
            saveLocation(out)
            resolve(out)
          } else {
            fallbackBrowser().then(resolve).catch(reject)
          }
        })
      })
    })
  } catch {
    return await fallbackBrowser()
  }
}

function fallbackBrowser(): Promise<LocationResult> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const out: LocationResult = {
          lng: pos.coords.longitude,
          lat: pos.coords.latitude,
          accuracy: pos.coords.accuracy,
          source: 'browser',
          timestamp: Date.now()
        }
        saveLocation(out)
        resolve(out)
      },
      (err) => {
        reject(new Error(err.message || '定位失败'))
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
    )
  })
}

