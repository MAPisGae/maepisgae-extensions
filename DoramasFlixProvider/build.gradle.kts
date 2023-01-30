// use an integer for version numbers
version = 1


cloudstream {
    language = "es"
    // All of these properties are optional, you can safely remove them

    //description = "Lorem Ipsum"
    authors = listOf("Stormunblessed")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "AsianDrama",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=doramasflix.co&sz=%size%"
}