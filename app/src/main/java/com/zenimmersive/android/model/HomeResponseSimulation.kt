package com.zenimmersive.android.model

object HomeResponseSimulation {
    fun getList(): List<ChildItem> {
        var dataList = arrayListOf<ChildItem>()
        var typeArray = arrayListOf("Popular", "Album", "Playlist", "ExploreType", "RecentlyPlayed")

        // Create a list of indices that will be used randomly but uniquely
        val availableIndices = typeArray.indices.toMutableList()

        availableIndices.forEach {
            // Get a random index from the available indices and remove it to ensure uniqueness
            val randomIndex = typeArray.indices.random()
            // Add the ChildItem at the random index
            dataList.add(it, ChildItem(typeArray[randomIndex]))
            typeArray.removeAt(randomIndex)
        }

        /*typeArray.forEach {
            // Get a random index from the available indices and remove it to ensure uniqueness
            val randomIndex = availableIndices.random()
            availableIndices.remove(randomIndex)
            // Add the ChildItem at the random index
            dataList.add(randomIndex, ChildItem(it))
        }*/

        return dataList
    }
}

data class ChildItem(val type : String)