export function flipCard(elementId) {
  var card = document.querySelector('#' + elementId)
  card?.classList?.toggle('is-flipped')
}
