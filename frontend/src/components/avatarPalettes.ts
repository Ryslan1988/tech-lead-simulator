/**
 * Palettes for the generated candidate portraits (see AvatarArt.vue).
 *
 * Keys are the file stem of `Candidate.avatarUrl` (e.g. `/assets/candidates/alexey.png`
 * -> `alexey`), not the candidate name: names arrive in different languages and cases,
 * the URL path is fixed by the API contract.
 *
 * The set covers BOTH sources of seed data, which do not use the same slugs:
 *   backend V2__seed.sql -> alexey, maria, dmitry, sergey
 *   frontend mocks       -> maria, alexey, igor, dmitriy
 * `dmitriy` and `dmitry` are the same person spelled two ways and share a palette.
 */
export interface AvatarPalette {
  bg: string
  skin: string
  hair: string
  shirt: string
  /** Draws an extra hair mass behind the shoulders. */
  longHair: boolean
}

const MARIA: AvatarPalette = {
  bg: '#e8eefc',
  skin: '#f2c9a8',
  hair: '#6b4230',
  shirt: '#ffffff',
  longHair: true,
}
const ALEXEY: AvatarPalette = {
  bg: '#fdeadf',
  skin: '#f0c19c',
  hair: '#a4532a',
  shirt: '#2f4a7a',
  longHair: false,
}
const IGOR: AvatarPalette = {
  bg: '#e6f4ec',
  skin: '#e8b98f',
  hair: '#2f2a26',
  shirt: '#d9762b',
  longHair: false,
}
const DMITRY: AvatarPalette = {
  bg: '#eef0f4',
  skin: '#f2c9a8',
  hair: '#c8a262',
  shirt: '#5a6472',
  longHair: false,
}
const SERGEY: AvatarPalette = {
  bg: '#fdf3e0',
  skin: '#e0aa80',
  hair: '#1f1c1a',
  shirt: '#3f7d54',
  longHair: false,
}

const PALETTES: Record<string, AvatarPalette> = {
  maria: MARIA,
  alexey: ALEXEY,
  igor: IGOR,
  dmitry: DMITRY,
  dmitriy: DMITRY,
  sergey: SERGEY,
}

/** `/assets/candidates/alexey.png` -> the ALEXEY palette; unknown slug -> null. */
export function paletteFor(avatarUrl?: string): AvatarPalette | null {
  if (!avatarUrl) return null
  const file = avatarUrl.split('/').pop() ?? ''
  const stem = file.replace(/\.[^.]+$/, '').toLowerCase()
  return PALETTES[stem] ?? null
}
