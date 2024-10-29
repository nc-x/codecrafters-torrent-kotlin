enum class PeerMessage(val id: Byte) {
    Choke(0),
    Unchoke(1),
    Interested(2),
    NotInterested(3),
    Have(4),
    Bitfield(5),
    Request(6),
    Piece(7),
    Cancel(8),
}

data class Peer(val id: String, val supportsExtensions: Boolean)
