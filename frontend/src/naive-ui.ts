import type { GlobalThemeOverrides } from 'naive-ui'

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#18a058',
    primaryColorHover: '#36ad6a',
    primaryColorPressed: '#0c7a43',
    primaryColorSuppl: '#36ad6a',
  },
  Layout: {
    siderColor: '#001529',
    headerColor: '#fff',
  },
  Menu: {
    itemTextColor: 'rgba(255, 255, 255, 0.65)',
    itemTextColorHover: '#fff',
    itemTextColorActive: '#fff',
    itemIconColor: 'rgba(255, 255, 255, 0.65)',
    itemIconColorHover: '#fff',
    itemIconColorActive: '#fff',
    itemColorActive: '#18a058',
    itemColorActiveHover: '#18a058',
  },
}

export const lightThemeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#18a058',
    primaryColorHover: '#36ad6a',
    primaryColorPressed: '#0c7a43',
    primaryColorSuppl: '#36ad6a',
  },
}
