<img src="https://i.imgur.com/mYI3qXM.png.png">

# Open Data (API)
[![Hits](https://hitcount.dev/p/glitchjsy/data-api.svg)](https://hitcount.dev/p/glitchjsy/data-api)  
The public API for accessing OpenData.je.je.

## Redis keys 
The keys which must be created manually can be done by running the `create-redis-keys.js` file in the `static` directory.

- `data-livespaces:json` - Stores live carpark spaces data, created automatically using `data-fetcher`
- `data-eatsafe:json` - Stores eatsafe information, created automatically using `data-fetcher`
- `data-toilets:json` - Static toilet data, must be created manually
- `data-recycling:json` - Static recycling data, must be created manually
- `data-defibrillators:json` - Static defibrillator locations, must be created manually
- `data-bus-passengers:json` - Static bus passengers data, must be created manually
- `data-road-traffic:json` - Static road traffic data, must be created manually
- `data-driving-test-results:json` - Static practical driving test results, must be created manually
- `data-monthly-rainfall:json` - Static monthly rainfall data, must be created manually
- `data-registered-vehicles:json` - Static registered vehicle counts per year, must be created manually

## All Projects
* [Frontend](https://github.com/glitchjsy/opendata.je) - The frontend / API docs for OpenData.je
* [API](https://github.com/glitchjsy/opendata-api) - Public API, located in this repo.
* [Fetcher](https://github.com/glitchjsy/opendata-fetcher) - A program to periodically fetch data.
* [Downloads](https://github.com/glitchjsy/opendata-downloads) - Daily snapshots of datasets from OpenData.je

---

Got a question? [Get in touch](mailto:luke@glitch.je)!
