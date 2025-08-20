const Copyright = ({company = "Speed Sneakers", year = new Date().getFullYear() }) => 
    <p className='copyright__text'>&copy; <span className='copyright__year'>{year}</span> <span className='copyright__company'>{company}</span>. Todos los derechos reservados.</p>;

export default Copyright;