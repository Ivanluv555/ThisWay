const cidiApi = require('./cidi');

const getFuturePrediction = (spotId) => cidiApi.getFuturePrediction(spotId);
const getNextDestinationForecast = (spotId) => cidiApi.getNextDestinationForecast(spotId);

module.exports = {
	getFuturePrediction,
	getNextDestinationForecast
};