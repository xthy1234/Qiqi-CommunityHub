
export interface BackgroundSource {
  id: string;
  type: 'direct' | 'json';
  url: string;
  name: string;
  params?: Record<string, string>;
  headers?: Record<string, string>;
}

export const MAX_USE_COUNT = 5;

export const BACKGROUND_SOURCES: BackgroundSource[] = [
  {
    id: 'smallbottle',
    type: 'direct',
    url: 'https://image.smallbottle.top/landscape',
    name: 'smallbottle'
  },
  {
    id: 'nekosia',
    type: 'json',
    url: 'https://api.nekosia.cat/api/v1/images/random',
    name: 'Nekosia Cute',
    params: {
      count: '1',
      rating: 'safe',
      session: 'ip'
    }
  },
  {
    id: 'nekos_best_neko',
    type: 'json',
    url: 'https://nekos.best/api/v2/neko',
    name: 'Nekos.best Neko',
    headers: {
      'User-Agent': 'Qiqi-CommunityHub/1.0 (Localhost)'
    }
  }
];

export const MAX_CACHE_AGE = 30 * 60 * 1000;
