package model

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import kotlin.collections.emptyList

@Entity
data class Language(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long = 0,
    val name: String,
    val version: String,
    @OneToMany(mappedBy = "language", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @JsonManagedReference
    val snippets: List<Snippet> = emptyList(),
) {
    constructor() : this(0, "", "", emptyList())

    override fun toString(): String {
        return "Language(id=$id, name='$name', version='$version')"
    }
}
