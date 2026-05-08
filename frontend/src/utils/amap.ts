import { getGaodeMapEnv, waitForAMap } from '../../gaode-map.config'

declare global {
  interface Window {
    _AMapSecurityConfig?: { securityJsCode: string }
    AMap?: any
  }
}

let loading: Promise<any> | null = null

export function loadAMap(): Promise<any> {
  if (window.AMap) return Promise.resolve(window.AMap)
  if (loading) return loading

  const { key, securityJsCode } = getGaodeMapEnv()
  if (!key || !securityJsCode) {
    return Promise.reject(new Error('缺少 VITE_AMAP_KEY 或 VITE_AMAP_SECURITY_JS_CODE'))
  }

  window._AMapSecurityConfig = { securityJsCode }
  const script = document.createElement('script')
  script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
  script.async = true
  document.head.appendChild(script)

  loading = waitForAMap()
  return loading
}

