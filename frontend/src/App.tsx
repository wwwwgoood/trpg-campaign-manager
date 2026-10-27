import { useState } from "react"
import CharacterDetail from "./components/CharacterDetail"
import CharacterList from "./components/CharacterList"
import CreateCharacterForm from "./components/CreateCharacterForm"

function App() {

  const [selectedCharacterId, setSelectedCharacterId] =
    useState<number | null>(null)

  const [refreshKey, setRefreshKey] = useState(0)

  const campaignId = 1
  const viewerMemberId = 2

  return (
    <div>
      <CharacterList
        campaignId={campaignId}
        viewerMemberId={viewerMemberId}
        refreshKey={refreshKey}
        onSelectCharacter={setSelectedCharacterId}
      />

      {selectedCharacterId !== null && (
        <CharacterDetail
          characterId={selectedCharacterId}
          viewerMemberId={viewerMemberId}
        />
      )}

      <CreateCharacterForm
        campaignId={campaignId}
        ownerMemberId={viewerMemberId}
        onCreated={() =>
          setRefreshKey(key => key + 1)
        }
      />
    </div>
  )
}

export default App