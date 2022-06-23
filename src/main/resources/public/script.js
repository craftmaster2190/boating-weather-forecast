const getLocationPromise = () => {
    return new Promise(function (resolve, reject) {

        // Promisifying the geolocation API
        navigator.geolocation.getCurrentPosition(
            (position) => resolve(position),
            (error) => reject(error)
        );
    });
};

const DAY_OF_WEEK = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

async function main() {
    const position = await getLocationPromise();
    console.log(position.coords);
    const response = await fetch(`best?results=8&latitude=${position.coords.latitude}&longitude=${position.coords.longitude}`);
    const body = await response.json();
    // console.log(body);
    Object.keys(body).sort()
        .map(date => {
            const dayOfWeek = DAY_OF_WEEK[new Date(date + "T00:00:00").getDay()];

            const data = body[date];

            const lakes = data.map(d => `<div class="lake">
<h3>${d.name}</h3>
<div>Score: ${Math.round(d.score)}</div>
<div>Air Temp: ${Math.round(d.highLowF.low)}-${Math.round(d.highLowF.high)}&deg; F</div>
<div>Water Temp: ${Math.round(d.waterConditions.highLowF.low)}-${Math.round(d.waterConditions.highLowF.high)}&deg; F</div>
<div>Wind Speed: ${Math.round(d.highLowWindSpeedMph.low)}-${Math.round(d.highLowWindSpeedMph.high)} mph</div>
<div>Precipitation: ${Math.round(d.highLowPrecipitationProbability.low)}-${Math.round(d.highLowPrecipitationProbability.high)} %</div>
<div>Distance: ${Math.round(d.distanceMiles)} miles</div>
</div>`).join("\n");

            jQuery("body").append(`<div class="date">
<h1>${date}</h1>
<h2>${dayOfWeek}</h2>
${lakes}
</div>`);
        });
    jQuery(".loading").remove();
}

function docReady(fn) {
    // see if DOM is already available
    if (document.readyState === "complete" || document.readyState === "interactive") {
        // call on next available tick
        setTimeout(fn, 1);
    } else {
        document.addEventListener("DOMContentLoaded", fn);
    }
}

docReady(main);

