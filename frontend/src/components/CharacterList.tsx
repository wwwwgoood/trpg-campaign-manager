import { useEffect, useState } from "react"
import { getCharacters } from "../api/characterApi"
import type { CharacterResponse } from "../types/character"

type Props = {
  campaignId: number
  viewerMemberId: number
  refreshKey: number
  onSelectCharacter: (characterId: number) => void
}

export default function CharacterList({
  campaignId,
  viewerMemberId,
  refreshKey,
  onSelectCharacter
}: Props) {

  const [characters, setCharacters] =
    useState<CharacterResponse[]>([])

  useEffect(() => {
  getCharacters(campaignId, viewerMemberId)
    .then(data => setCharacters(data))
}, [campaignId, viewerMemberId, refreshKey])

  return (
    <div>
      <h2>Characters</h2>

      {characters.map(character => (
        <button
          key={character.id}
          onClick={() =>
            onSelectCharacter(character.id)
          }
        >
          {character.name}
        </button>
      ))}
    </div>
  )
}