import { useEffect, useState } from "react"
import { getCharacter } from "../api/characterApi"
import type { CharacterResponse } from "../types/character"
import EditCharacterForm from "./EditCharacterForm"

type Props = {
  characterId: number
  viewerMemberId: number
}

export default function CharacterDetail({
  characterId,
  viewerMemberId
}: Props) {

  const [character, setCharacter] =
    useState<CharacterResponse | null>(null)

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  async function loadCharacter() {
    setLoading(true)
    setError(null)

    try {
      const data = await getCharacter(
        characterId,
        viewerMemberId
      )

      setCharacter(data)

    } catch (error) {
      if (error instanceof Error) {
        setError(error.message)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadCharacter()
  }, [characterId, viewerMemberId])

  if (loading) {
    return <p>Loading...</p>
  }

  if (error) {
    return <p>{error}</p>
  }

  if (!character) {
    return <p>No character</p>
  }

  return (
    <div>
      <h2>{character.name}</h2>

      <p>
        HP: {character.currentHp} / {character.maxHp}
      </p>

      <p>
        Cash:
        {character.cashCents.visible
          ? character.cashCents.value
          : "Hidden"}
      </p>

      <p>
        Desire:
        {character.desire.visible
          ? character.desire.value
          : "Hidden"}
      </p>

      <p>
        Secret:
        {character.secret.visible
          ? character.secret.value
          : "Hidden"}
      </p>

      <EditCharacterForm
        character={character}
        viewerMemberId={viewerMemberId}
        onUpdated={loadCharacter}
      />
    </div>
  )
}