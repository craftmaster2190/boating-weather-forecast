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
    jQuery(".button-logan").click(() => getFor("41.73853621944938", "-111.83359421547772"));
    jQuery(".button-salt-lake-city").click(() => getFor("40.66762793905019", "-111.95243078022585"));
    jQuery(".button-cedar-city").click(() => getFor("37.72735784248237", "-113.05370254451867"));

    const position = await getLocationPromise();
    return getFor(position.coords.latitude, position.coords.longitude)
}

async function getFor(latitude, longitude) {
    const allButtons = jQuery("button");
    jQuery(".loading-text").text("Loading").show()
    allButtons.attr('disabled','disabled');
    console.log(latitude, longitude);
    const response = await fetch(`best?results=8&latitude=${latitude}&longitude=${longitude}`);
    const body = await response.json();
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

            jQuery(".content").append(`<div class="date">
<h1>${date}</h1>
<h2>${dayOfWeek}</h2>
${lakes}
</div>`);
        });
    jQuery(".loading").hide();
    allButtons.removeAttr('disabled');
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

