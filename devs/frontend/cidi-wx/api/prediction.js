const cidiApi = require('./cidi');

const getFuturePrediction = (spotId) => cidiApi.getFuturePrediction(spotId);

module.exports = {
	getFuturePrediction
};