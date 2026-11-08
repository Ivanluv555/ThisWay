import { useId } from 'react';

/** Original editorial illustration. Not a geographic map or an actual scenic photograph. */
export function Landscape({ variant = 'city', className = '' }: {
  variant?: 'city' | 'mountain' | 'town'; className?: string;
}) {
  const id = useId().replace(/:/g, '');
  return (
    <svg className={`landscape ${className}`} viewBox="0 0 640 700" preserveAspectRatio="xMidYMid slice" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true" focusable="false">
      <defs>
        <linearGradient id={`${id}-sky`} x1="320" y1="0" x2="320" y2="700" gradientUnits="userSpaceOnUse">
          <stop stopColor="#E8E8DA" /><stop offset="1" stopColor="#F6EFE0" />
        </linearGradient>
        <linearGradient id={`${id}-river`} x1="400" y1="340" x2="300" y2="700" gradientUnits="userSpaceOnUse">
          <stop stopColor="#94B5A6" /><stop offset="1" stopColor="#D8DFCC" />
        </linearGradient>
        <pattern id={`${id}-windows`} width="22" height="23" patternUnits="userSpaceOnUse">
          <rect x="7" y="7" width="7" height="10" rx="1" fill="#EBC78A" />
        </pattern>
      </defs>
      <path fill={`url(#${id}-sky)`} d="M0 0h640v700H0z" />
      <circle cx="454" cy="155" r="65" fill="#DDA864" opacity=".85" />
      <path d="M-35 330 81 155l82 114 87-154 118 192 68-105 154 162 86-115v451H-35Z" fill="#C7CDB9" />
      <path d="m-33 399 121-147 90 77 138-121 144 188 112-129 112 173v260H-33Z" fill="#A5B7A3" />
      <path d="M-30 448c122-131 191-119 292-15 88 91 183 28 217-11 52-60 104-74 197-15v293H-30Z" fill="#789786" />
      <path d="M675 381c-115 58-109 119-263 131-145 11-188 67-194 188h457Z" fill={`url(#${id}-river)`} />
      <path d="M658 422c-74 23-87 75-208 99M595 541c-90 10-181 42-191 78m116-46-48 13m-83 66-9 21" stroke="#F8F3DE" strokeWidth="2" strokeLinecap="round" opacity=".65" />
      {variant === 'city' && <>
        <g fill="#88A08E">
          <path d="M368 313h32v96h-32zm44-40h30v137h-30zm41 30h25v108h-25zm39-59h36v165h-36zm48 55h27v109h-27z" />
          <path d="M423 253h8v20h-8zm83-28h8v19h-8z" />
        </g>
        <path d="M403 433c78-73 148-68 254-8M403 434h254" stroke="#E6E8D7" strokeWidth="6" />
        <path d="M430 414v21m32-41v41m34-50v50m34-48v48m34-39v39m34-24v24m33-13v13" stroke="#E6E8D7" strokeWidth="3" />
        <path d="M-11 407c124-64 201-33 325 54v239H-11Z" fill="#55755F" />
        <g stroke="#405C49" strokeWidth="3" strokeLinejoin="round">
          <path d="M56 390h106v93H56z" fill="#A47A50" /><path d="M56 390h106v93H56z" fill={`url(#${id}-windows)`} />
          <path d="m42 391 67-28 68 28Z" fill="#465D49" />
          <path d="M110 453h151v108H110z" fill="#AE875A" /><path d="M110 453h151v108H110z" fill={`url(#${id}-windows)`} />
          <path d="m93 453 94-35 89 35Z" fill="#3B5644" />
          <path d="M148 421h77v30h-77z" fill="#AE875A" /><path d="m133 421 55-27 54 27Z" fill="#3B5644" />
          <path d="M36 521h109v100H36z" fill="#98784F" /><path d="M36 521h109v100H36z" fill={`url(#${id}-windows)`} />
          <path d="m21 521 69-30 70 30Z" fill="#314C3D" />
          <path d="M184 574h105v88H184z" fill="#AB8251" /><path d="M184 574h105v88H184z" fill={`url(#${id}-windows)`} />
          <path d="m167 574 69-33 70 33Z" fill="#314C3D" />
        </g>
        <path d="M110 484v36m20-36v36m-83 102v43m19-43v43m129-4v39m27-39v39m47-140v38" stroke="#304D3D" strokeWidth="6" />
        <path d="M-30 73 680 300" stroke="#486451" strokeWidth="2" />
        <path d="M298 178v34m8-32v31" stroke="#486451" strokeWidth="3" />
        <rect x="273" y="211" width="61" height="44" rx="9" fill="#BA6147" />
        <path d="M280 217h16v18h-16zm22 0h24v18h-24z" fill="#E9E9CF" />
        <path d="M277 246h53" stroke="#873E30" strokeWidth="3" />
      </>}
      {variant === 'mountain' && <>
        <path d="M33 700V352l74-158 52 21 26 145-40 88v252Z" fill="#507461" />
        <path d="m102 700 49-283 35-19 36-156 53-51 62 51 21 136-49 109-6 213Z" fill="#355A48" />
        <path d="m397 700 1-221 30-99 16-169 52-90 53 26 53 213 38 51v289Z" fill="#527662" />
        <path d="M173 449c104-109 202-107 284-32l-6 57c-96-48-185-63-283 28Z" fill="#66846B" />
        <path d="m213 318 36-68m226-28 20-42m-51 176 14-89M76 367l32-95" stroke="#8BA387" strokeWidth="3" />
        <path d="M378 700c-91-106-1-140-41-195" stroke="#DED5AA" strokeWidth="5" />
      </>}
      {variant === 'town' && <>
        <path d="M-10 439c102-83 184-72 313 49v212H-10Z" fill="#52745C" />
        {[{ x: 40, y: 365 }, { x: 150, y: 440 }, { x: 50, y: 535 }, { x: 225, y: 550 }].map(({ x, y }) => <g key={x + y}>
          <path d={`M${x} ${y}h100v95H${x}z`} fill="#D4BA8B" />
          <path d={`M${x} ${y}h100v95H${x}z`} fill={`url(#${id}-windows)`} />
          <path d={`m${x - 15} ${y} 65-35 65 35Z`} fill="#3B5344" />
          <path d={`M${x + 35} ${y + 50}h30v45h-30z`} fill="#5B5B41" />
        </g>)}
        <path d="M328 700c-78-94-90-123-17-183" stroke="#D9CBA7" strokeWidth="19" />
      </>}
      <path d="M-10 644c88-55 162 24 222 56H-10Zm461 56c59-120 133-149 206-169v169Z" fill="#2D5140" />
      <path d="m554 700-21-164m13 100-40-38m36 10 31-42m-30 57 50-21" stroke="#244633" strokeWidth="5" />
      <g fill="#395D45"><ellipse cx="520" cy="561" rx="22" ry="43" transform="rotate(-32 520 561)" /><ellipse cx="567" cy="568" rx="22" ry="39" transform="rotate(37 567 568)" /><ellipse cx="589" cy="615" rx="24" ry="44" transform="rotate(55 589 615)" /></g>
      <path d="m76 116 8-4 8 4m30 20 8-4 8 4" stroke="#708575" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}
