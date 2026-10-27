import type { CharacterResponse } from "../types/character"

export async function getCharacter(
  characterId: number,
  viewerMemberId: number
): Promise<CharacterResponse> {

  const response = await fetch(
    `http://localhost:8080/api/characters/${characterId}?viewerMemberId=${viewerMemberId}`
  )

  if (!response.ok) {
    throw new Error("Failed to load character")
  }

  return response.json()
}

export async function getCharacters(
  campaignId: number,
  viewerMemberId: number
): Promise<CharacterResponse[]> {

  const response = await fetch(
    `http://localhost:8080/api/campaigns/${campaignId}/characters?viewerMemberId=${viewerMemberId}`
  )

  if (!response.ok) {
    throw new Error("Failed to load characters")
  }

  return response.json()
}