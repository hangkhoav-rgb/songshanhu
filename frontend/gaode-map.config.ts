export interface GaodeMapEnv {
  key: string
  securityJsCode: string
}

export function getGaodeMapEnv(): GaodeMapEnv {
  const key = (import.meta as any).env?.VITE_AMAP_KEY as string || ''
  const securityJsCode = (import.meta as any).env?.VITE_AMAP_SECURITY_JS_CODE as string || ''
  return { key, securityJsCode }
}

export function assertGaodeMapEnv() {
  const { key, securityJsCode } = getGaodeMapEnv()
  if (!key) {
    throw new Error('Missing env: VITE_AMAP_KEY')
  }
  if (!securityJsCode) {
    throw new Error('Missing env: VITE_AMAP_SECURITY_JS_CODE')
  }
}

export function isGaodeMapConfigured() {
  const { key, securityJsCode } = getGaodeMapEnv()
  return Boolean(key && securityJsCode)
}

export function waitForAMap(timeoutMs = 15000): Promise<any> {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    const tick = () => {
      const AMap = (window as any).AMap
      if (AMap) {
        resolve(AMap)
        return
      }
      if (Date.now() - start > timeoutMs) {
        reject(new Error('AMap SDK not loaded'))
        return
      }
      setTimeout(tick, 50)
    }
    tick()
  })
}

